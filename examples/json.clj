{root=    (space value space $)
 value    ((string / number / boolean / array / null / object) >>)
 string   ("\"" > (:string #"(\\\"|[^\"])*") "\"")
 number=  #"-?\d+(\.\d+)?"
 boolean= ("true" / "false")
 array=   ("[" > space (value (space "," > space value)*)? space "]")
 null=    "null"
 object=  ("{" > space (entry (space "," > space entry)*)? space "}")
 entry=   (string > space ":" > space value)
 space    #"\s*"}
