;; Not complete (yet)
{root= ((spacing form spacing)* $)
 form  (list / vector / map / set / string / regex / symbol / keyword / number / character / quote / meta)

 list=   ("(" > (spacing form spacing)* ")")
 vector= ("[" > (spacing form spacing)* "]")
 map=    ("{" > (spacing form spacing)* "}")
 set=    ("#{" > (spacing form spacing)* "}")

 string= ("\"" > #"(\\\"|[^\"])*" "\"")
 regex=  ("#\"" > #"(\\\"|[^\"])*" "\"")

 sym-start  #"[a-zA-Z-_\.<>*+=!$%&?/]"
 sym-more   #"[0-9':]"
 symbol=    (sym-start (sym-start / sym-more)*)
 keyword=   (#":{1,2}" > symbol)

 long=      #"\d+"
 radix=     #"[2-9][0-9]?r\d+"
 hex=       #"0x\d+"
 number     (long / radix / hex)

 character= #"\\\S"

 quote= ("'" form)
 meta=  ("^" (keyword / map))

 spacing #"([\s,]*(;.*)?)*"}
