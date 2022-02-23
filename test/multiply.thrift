struct Numbers {
	1: i32 x=0,
	2: i32 y
}
service Multiply {
	bool ping(),
	i32 multiply_1(1: Numbers nums),
	i32 multiply_2(1: i32 x, 2:i32 y)
}	
