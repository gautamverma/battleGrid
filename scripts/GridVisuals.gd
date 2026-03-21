extends Node2D

const CELL_SIZE = 40
const GRID_SIZE = 10

var grid_color: Color = Color(0.2, 0.8, 1.0, 0.5) # Neon blueish
var hover_color: Color = Color(1.0, 1.0, 1.0, 0.3)

var hovered_cell: Vector2 = Vector2(-1, -1)

func _ready():
	queue_redraw()

func _draw():
	# Draw cells
	for x in range(GRID_SIZE):
		for y in range(GRID_SIZE):
			var rect = Rect2(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE)
			
			if Vector2(x, y) == hovered_cell:
				draw_rect(rect, hover_color, true)
				
			draw_rect(rect, grid_color, false, 2.0) # Line width 2

func _input(event):
	if event is InputEventMouseMotion:
		var local_pos = get_local_mouse_position()
		var cx = int(local_pos.x / CELL_SIZE)
		var cy = int(local_pos.y / CELL_SIZE)
		
		if cx >= 0 and cx < GRID_SIZE and cy >= 0 and cy < GRID_SIZE:
			if hovered_cell != Vector2(cx, cy):
				hovered_cell = Vector2(cx, cy)
				queue_redraw()
		else:
			if hovered_cell != Vector2(-1, -1):
				hovered_cell = Vector2(-1, -1)
				queue_redraw()
