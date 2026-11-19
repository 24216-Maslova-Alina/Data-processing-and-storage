(defn pfilter-blocks [pred blocks workers]
  (lazy-seq
   (when (seq blocks)
     (let [current (take workers blocks)
           tasks (doall (map (fn [block]
                               (future (doall (filter pred block))))
                             current))
           results (map deref tasks)
           other-blocks (drop workers blocks)]
       (concat (apply concat results)
               (pfilter-blocks pred other-blocks workers))))))

(defn pfilter [pred coll block-size workers]
  (pfilter-blocks pred (partition-all block-size coll) workers))

(println (take 10 (pfilter even? (iterate inc 0) 5 4)))


; Тесты
(require '[clojure.test :refer [deftest is run-tests]])

;; Корректность на конечной последовательности
(deftest test-finite
  (is (= (filter even? (range 1000))
         (pfilter even? (range 1000) 10 4))))

;; Пустой вход:
(deftest test-empty
  (is (= (list) (pfilter even? (list) 10 4))))

;; Неполный последний блок: (range 7) с block-size=3
(deftest test-last-block
  (is (= (filter odd? (range 7))
         (pfilter odd? (range 7) 3 2))))

;; Бесконечная последовательность + take:
(deftest test-infinite
  (is (= (list 0 2 4 6 8)
         (take 5 (pfilter even? (iterate inc 0) 10 4)))))

(run-tests)


; Демонстрация скорости
(defn slow-even? [x]
  (Thread/sleep 1)
  (even? x))

(println "обычный filter:")
(time (doall (filter slow-even? (range 2000))))

(println "pfilter:")
(time (doall (pfilter slow-even? (range 2000) 50 8)))
(shutdown-agents)