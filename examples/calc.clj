{sum ((:sum product sum-op > sum) / product)

 product ((:product value product-op > product) / value)

 value (number / "(" > sum ? ")")

 sum-op (:operation #"[+-]")

 product-op (:operation #"[*/]")

 number= #"[0-9]+"}
