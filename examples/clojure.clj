;; Support for tags, reader literals, syntax-quote and splice is missing
{root= ((spacing form spacing)* $)
 form  (list / vector / map / set / string / regex / symbol / keyword / number / character / quote / meta / comma)

 list=   ("(" > (spacing form spacing)* ")")
 vector= ("[" > (spacing form spacing)* "]")
 map=    ("{" > (spacing form spacing)* "}")
 set=    ("#{" > (spacing form spacing)* "}")

 string= ("\"" > #"(\\\"|[^\"])*" "\"")
 regex=  ("#\"" > #"(\\\"|[^\"])*" "\"")

 symbol=    #"[a-zA-Z-_\.<>*+=!$%&?/][a-zA-Z0-9-_\.<>*+=!$%&?/]*"
 keyword=   (#":{1,2}" > symbol)
 number=    #"\d+"
 character= #"\\\S"

 quote= ("'" form)
 meta=  ("^" (keyword / map))

 comma ","

 spacing #"(\s*(;.*)?)*"}
