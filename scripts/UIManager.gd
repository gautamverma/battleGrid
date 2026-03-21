extends Control
class_name UIManager

enum GamePhase { MAIN_MENU, PLACEMENT, COMBAT, GAME_OVER }
var current_phase: GamePhase = GamePhase.MAIN_MENU

# UI Containers that would be assigned via standard Editor Node Paths
@onready var main_menu_panel = $MainMenuPanel
@onready var placement_panel = $PlacementPanel
@onready var combat_panel = $CombatPanel
@onready var game_over_panel = $GameOverPanel
@onready var status_label = $CombatPanel/StatusLabel

func _ready():
	_switch_phase(GamePhase.MAIN_MENU)
	
	# Setup networking associations
	BluetoothManager.connection_established.connect(_on_connected)
	BluetoothManager.connection_lost.connect(_on_connection_lost)
	BluetoothManager.opponent_ready.connect(_on_opponent_ready)
	BluetoothManager.receive_game_over.connect(_on_receive_game_over)

func _switch_phase(new_phase: GamePhase) -> void:
	current_phase = new_phase
	
	if main_menu_panel: main_menu_panel.visible = (new_phase == GamePhase.MAIN_MENU)
	if placement_panel: placement_panel.visible = (new_phase == GamePhase.PLACEMENT)
	if combat_panel: combat_panel.visible = (new_phase == GamePhase.COMBAT)
	if game_over_panel: game_over_panel.visible = (new_phase == GamePhase.GAME_OVER)

# --- Button Handlers ---

func _on_HostButton_pressed() -> void:
	BluetoothManager.host_game()
	if status_label: status_label.text = "Waiting for player to join..."

func _on_JoinButton_pressed() -> void:
	var mac = "00:11:22:33:44:55" # Example MAC
	BluetoothManager.join_game(mac)
	if status_label: status_label.text = "Connecting..."

func _on_ReadyButton_pressed() -> void:
	# Placement phase finished by local player
	local_player_ready_up()
	if status_label: status_label.text = "Waiting for Opponent..."

# --- Subscribed Events ---

func _on_connected() -> void:
	print("Connected! Entering placement phase.")
	_switch_phase(GamePhase.PLACEMENT)

func _on_connection_lost() -> void:
	print("Connection lost. Returning to main menu.")
	_switch_phase(GamePhase.MAIN_MENU)
	# Here you'd likely want to show an alert popup

var is_opponent_ready = false
var am_i_ready = false

func _on_opponent_ready() -> void:
	is_opponent_ready = true
	_check_start_combat()

func local_player_ready_up() -> void:
	am_i_ready = true
	BluetoothManager.send_ready()
	_check_start_combat()

func _check_start_combat() -> void:
	if am_i_ready and is_opponent_ready:
		_switch_phase(GamePhase.COMBAT)
		# Handshake completion: determine who goes first
		# E.g., The Host player always goes first
		_update_status_label()

func _update_status_label() -> void:
	if current_phase == GamePhase.COMBAT and status_label:
		# Assuming we update text based on GameManager.is_my_turn
		status_label.text = "Game Started!"

func _on_receive_game_over() -> void:
	# We received a game over signal meaning opponent lost all their ships
	_show_game_over(true)

func _show_game_over(victory: bool) -> void:
	_switch_phase(GamePhase.GAME_OVER)
	var result_label = $GameOverPanel/ResultLabel
	if result_label:
		if victory:
			result_label.text = "VICTORY!"
		else:
			result_label.text = "DEFEAT"
