extends Node

const GRID_SIZE = 10

# Grid states: 
# 0 = Empty
# >=100 = Ship ID
# -1 = Miss 
# -2 = Destroyed Ship Part
var player_grid: Array = []
var opponent_grid: Array = [] # Only stores known hits/misses to maintain Fog of War.

# Maps ship_id -> Array of Vector2 coordinates
var ships: Dictionary = {}

var player_ships_alive: int = 3
var opponent_ships_alive: int = 3
var is_my_turn: bool = false

signal game_over(is_victory: bool)
signal grid_updated() # For UI refreshing

func _ready():
	_init_grids()
	# Connect to network signals for synchronized play
	BluetoothManager.receive_attack_coordinate.connect(_on_opponent_attacked)
	BluetoothManager.receive_attack_result.connect(_on_attack_result_received)

func _init_grids() -> void:
	player_grid.clear()
	opponent_grid.clear()
	ships.clear()
	player_ships_alive = 3
	opponent_ships_alive = 3
	
	for x in range(GRID_SIZE):
		var p_row = []
		var o_row = []
		for y in range(GRID_SIZE):
			p_row.append(0)
			o_row.append(0)
		player_grid.append(p_row)
		opponent_grid.append(o_row)

# Validates and places a ship during the Placement Phase
func place_ship(ship_id: int, start_coord: Vector2, bound_size: Vector2) -> bool:
	var coords = []
	for x in range(int(bound_size.x)):
		for y in range(int(bound_size.y)):
			var px = int(start_coord.x) + x
			var py = int(start_coord.y) + y
			
			# Check bounds
			if px >= GRID_SIZE or py >= GRID_SIZE or px < 0 or py < 0:
				return false
			# Check overlap
			if player_grid[px][py] != 0:
				return false
				
			coords.append(Vector2(px, py))
			
	# Commit placement
	ships[ship_id] = coords
	for c in coords:
		player_grid[int(c.x)][int(c.y)] = ship_id
		
	emit_signal("grid_updated")
	return true

# --- Combat Logic ---

# We initiated an attack
func fire_missile(coord: Vector2) -> void:
	if not is_my_turn: return
	
	# Can't fire at the exact same spot twice
	var cx = int(coord.x)
	var cy = int(coord.y)
	if opponent_grid[cx][cy] != 0:
		return
		
	is_my_turn = false
	BluetoothManager.send_attack(coord)

# Opponent attacked us
func _on_opponent_attacked(coord: Vector2) -> void:
	var cx = int(coord.x)
	var cy = int(coord.y)
	
	var cell_value = player_grid[cx][cy]
	
	if cell_value >= 100: # It's a ship
		var hit_ship_id = cell_value
		_destroy_ship(hit_ship_id)
		
		# Send result back
		BluetoothManager.send_hit_result(coord, true, hit_ship_id)
	else:
		# Miss
		if player_grid[cx][cy] == 0:
			player_grid[cx][cy] = -1
		BluetoothManager.send_hit_result(coord, false, 0)
		
	is_my_turn = true
	emit_signal("grid_updated")

# Result of our own attack
func _on_attack_result_received(coord: Vector2, is_hit: bool, destroyed_ship_id: int) -> void:
	var cx = int(coord.x)
	var cy = int(coord.y)
	
	if is_hit:
		opponent_grid[cx][cy] = destroyed_ship_id
		opponent_ships_alive -= 1
		print("We destroyed opponent ship: ", destroyed_ship_id)
		if opponent_ships_alive <= 0:
			emit_signal("game_over", true)
	else:
		opponent_grid[cx][cy] = -1 # Miss
		
	emit_signal("grid_updated")

# Chain reaction: Any single hit destroys the entire ship
func _destroy_ship(ship_id: int) -> void:
	if not ships.has(ship_id):
		return
		
	var ship_coords = ships[ship_id]
	for c in ship_coords:
		# Mark as destroyed
		player_grid[int(c.x)][int(c.y)] = -2
	
	ships.erase(ship_id)
	player_ships_alive -= 1
	
	if player_ships_alive <= 0:
		emit_signal("game_over", false)
		BluetoothManager.send_game_over()
