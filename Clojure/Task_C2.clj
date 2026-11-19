(defn sieve [numbers]
  (lazy-seq
   (let [p (first numbers)]
     (cons p
           (sieve (remove (fn [x] (= 0 (mod x p)))
                          (rest numbers)))))))

(def primes (sieve (iterate inc 2)))

(println (take 100 primes))


; Тесты
(require '[clojure.test :refer [deftest is run-tests]])

(deftest test-first-primes
  (is (= (list 2 3 5 7 11 13 17 19 23 29)
         (take 10 primes))))

(deftest test-first-prime
  (is (= 2 (first primes))))

(deftest test-hundredth-prime
  (is (= 541 (nth primes 99))))

(run-tests)