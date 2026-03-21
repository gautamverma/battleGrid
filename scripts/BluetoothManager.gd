extends Node
# Autoloaded as 'BluetoothManager'

# Signals for game events synced over network
signal connection_established()
signal connection_lost()
signal receive_attack_coordinate(coord: Vector2)
signal receive_attack_result(coord: Vector2, is_hit: bool, destroyed_ship_id: int)
signal opponent_ready()
signal receive_game_over()

enum NetworkState { DISCONNECTED, HOSTING, SEARCHING, CONNECTED }
var current_state: NetworkState = NetworkState.DISCONNECTED
var is_host: bool = false

# Placeholder reference to a native Godot Android Bluetooth plugin
# Godot 4 accesses singletons using Engine.get_singleton()
var bluetooth_plugin: Object = null

func _ready():
	if Engine.has_singleton("GodotBluetooth"):
		bluetooth_plugin = Engine.get_singleton("GodotBluetooth")
		bluetooth_plugin.data_received.connect(_on_data_received)
		bluetooth_plugin.connected.connect(_on_connected)
		bluetooth_plugin.disconnected.connect(_on_disconnected)

func host_game() -> void:
	current_state = NetworkState.HOSTING
	is_host = true
	print("Hosting game via Bluetooth...")
	if bluetooth_plugin:
		bluetooth_plugin.start_server()

func join_game(mac_address: String) -> void:
	current_state = NetworkState.SEARCHING
	is_host = false
	print("Joining game with MAC: ", mac_address)
	if bluetooth_plugin:
		bluetooth_plugin.connect_to_device(mac_address)

func disconnect_game() -> void:
	if bluetooth_plugin and current_state != NetworkState.DISCONNECTED:
		bluetooth_plugin.disconnect_device()
	_on_disconnected()

func _on_connected() -> void:
	current_state = NetworkState.CONNECTED
	emit_signal("connection_established")

func _on_disconnected() -> void:
	current_state = NetworkState.DISCONNECTED
	emit_signal("connection_lost")

# -- Data Transmission Methods --

# Generic abstraction layer wrapper for formatting and sending
func send_data(dict_data: Dictionary) -> void:
	if current_state != NetworkState.CONNECTED:
		push_error("Cannot send data, not connected.")
		return
		
	var json_string = JSON.stringify(dict_data)
	if bluetooth_plugin:
		bluetooth_plugin.send_data(json_string.to_utf8_buffer())
	else:
		print("Mock Sending: ", json_string) # Debug

func _on_data_received(data: PackedByteArray) -> void:
	var text = data.get_string_from_utf8()
	var parsed = JSON.parse_string(text)
	if parsed != null and parsed is Dictionary:
		_handle_message(parsed)
	else:
		push_error("Failed to parse incoming Bluetooth data.")

func _handle_message(msg: Dictionary) -> void:
	match msg.get("type", ""):
		"attack":
			var coord = Vector2(msg["x"], msg["y"])
			emit_signal("receive_attack_coordinate", coord)
		"attack_result":
			var coord = Vector2(msg["x"], msg["y"])
			emit_signal("receive_attack_result", coord, msg["is_hit"], msg["ship_id"])
		"ready":
			emit_signal("opponent_ready")
		"game_over":
			emit_signal("receive_game_over")
		_:
			push_warning("Received unknown network message type.")

# -- Specific Payload Senders --

func send_attack(coord: Vector2) -> void:
	send_data({"type": "attack", "x": coord.x, "y": coord.y})

func send_hit_result(coord: Vector2, is_hit: bool, ship_id: int) -> void:
	send_data({"type": "attack_result", "x": int(coord.x), "y": int(coord.y), "is_hit": is_hit, "ship_id": ship_id})

func send_ready() -> void:
	send_data({"type": "ready"})

func send_game_over() -> void:
	send_data({"type": "game_over"})
