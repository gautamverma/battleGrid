extends ColorRect

var ship_id: int = 100
var bounds: Vector2 = Vector2(3, 1) # Default 3x1 ship
const CELL_SIZE = 40

func _ready():
	color = Color(1.0, 0.2, 0.5, 0.8) # Neon pink
	update_size()

func update_size():
	size = bounds * CELL_SIZE
	
	# Optional: draw outline by adding a child or using _draw
