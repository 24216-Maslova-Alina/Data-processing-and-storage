(defn sieve [numbers]
  (lazy-seq
   (let [p (first numbers)]
     (cons p
           (sieve (remove (fn [x] (= 0 (mod x p)))
                          (rest numbers)))))))

(def primes (sieve (iterate inc 2)))

(println (take 100 primes))