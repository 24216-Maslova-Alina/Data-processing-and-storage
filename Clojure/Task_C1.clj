(defn add-letter [words alphabet]
  (reduce
   (fn [result word]
     (concat result
             (map (fn [letter] (str word letter))
                  (remove (fn [letter] (= letter (str (last word))))
                          alphabet))))
   (list)
   words))

(defn make-words [alphabet n]
  (reduce
   (fn [words _] (add-letter words alphabet))
   alphabet
   (range (- n 1))))

(println (make-words (list "a" "b" "c") 2))
(println (make-words (list "a" "b" "c") 3))