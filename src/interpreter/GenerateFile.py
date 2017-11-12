# -*- coding: utf-8 -*-

def check(x):              #This is all written in 2.7 python. Be warned.
	if x < 0:
		return 0
	if x > 80:
		return 0
	if x == 0:
		return 1
	return x

file = open('gameOfLife.tom','w')
stringOpening = "let " 
stringPhrase = "position"
stringOutput = ""
output = 0
i = 0

file.write("\nlet x = 0\n")

file.write("let value = 0")

file.write("\nprint(x)\n")

for output in range(81):  #Initializes the numbers
	stringOutput = stringOpening + stringPhrase+str(output) + " = 0\n"
	file.write(stringOutput)

output = 0

file.write("\nprint(x)\n")

for output in range(81):  #Initializes the numbers
	stringOutput = stringOpening + "future"+stringPhrase+str(output) + " = 0\n"
	file.write(stringOutput)

file.write("\nwhile x < 10 do\n")

output = 80

while output >= 0:
	file.write("\nprint(x)\n")
	if check(output) != 0:
		while i < 3:
			file.write("value = ")
			if check(output + 1) != 0:
				file.write(stringPhrase+str(output + 1) + " +")
			if check(output - 1) != 0:
				file.write(" " + stringPhrase+str(output - 1) + " +")
			if check(output + 9) != 0:
				file.write(" " + stringPhrase+str(output + 9) + " +")
			if check(output - 9) != 0:
				file.write(" " + stringPhrase+str(output - 9) + " +") 
			if check(output + 1 + 9) != 0:
				file.write(" " + stringPhrase+str(output + 1 + 9) + " +")

			if check(output + 1 - 9) != 0:
				file.write(" " + stringPhrase+str(output + 1 - 9) + " +")

			if check(output - 1 + 9) != 0:
				file.write(" " + stringPhrase+str(output - 1 + 9) + " +")

			if check(output - 1 - 9) != 0:
				file.write(" " + stringPhrase+str(output - 1 - 9) + " +")
			file.write("0\n")

			if i == 0:
				file.write("if value < 2 and value > 0 then\n")
				file.write("future"+stringPhrase+str(output)+" = 0")
				file.write("\nend\n\n")
			if i == 1:
				file.write("if value < 4 and value > 1 then\n")
				file.write("future"+stringPhrase+str(output)+" = 1")
				file.write("\nend\n\n")
			if i == 2:
				file.write("if value > 3 then\n")
				file.write("future"+stringPhrase+str(output)+" = 0")
				file.write("\nend\n\n")
			i = i + 1
	i = 0
	output = output - 1

	print(output)

i = 0
output = 0

file.write("\nprint(i)\n")

while i < 81:
	file.write(stringPhrase+str(i)+"="+"future"+stringPhrase+str(i)+"\n")
	i = i + 1

file.write("\nx = x + 1\n")


file.write("\nprint(x)\n")
file.write("\nend   ")

file.close()
