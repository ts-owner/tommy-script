## things to do still


	1.	loop grammar - Finished
	
	2.	fix the types thing (user defined types??)
	
	3.	generalize the parser thing (too hard)
	
	4.	add array operators - done sloppily
	
	5.	lambdas
	
	6.	logo -(v.1 done)
    
    	7.  	if expressions - done

## tree-walk interpretr specific things

	1.	refactor recursive function into two functions. one that takes expr->literal, one that takes statement->Unit
	
	2.	get rid of tuples and use data classes
	
	3. 	actual error control

		1.	when you reinitialize existing variable
		
		2. 	try to call non-existing func needs to error

		3.	uninitialized variable error DONE
	
	4.	integrate typechecking pre-pass step

	5. 	make functions be a List<Func> that is separate from environment

	6.	Lower priority ones
		1.	fix power function

		2.	handle doubles and Ints
		
		3.	string concat DONE

		4.	user defined types

		5.	comments

		6.	issue with concat operator and strings, also strings in array literals

		7.	for loops
	
		8.	property methods

