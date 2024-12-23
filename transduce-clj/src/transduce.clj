(ns transduce
  (:require
   [clojure.core.async :refer [<!! >!! chan close!]]
   [clojure.core.protocols :as protocols]
   [criterium.core :refer [quick-bench]]))

(set! *warn-on-reflection* true)

;; Evaluates to a lazy seq of the five even numbers after the first five.
;; Each of the steps in this computation produces an intermediate lazy seq.

(->> (range)
     (filter even?)
     (drop 5)
     (take 5))

;; Evaluates to a vector containing the five even numbers after the first five.
;; The result is computed eagerly, using a transducer.

(into []
      (comp
       (filter even?)
       (drop 5)
       (take 5))
      (range))

;; Transducers can be used as part of an eduction. You could see an eduction
;; as something that is not yet a reduction, i.e. something that must still
;; be reduced.

(eduction
 (comp
  (filter even?)
  (drop 5)
  (take 5))
 (range))

(comment
  (def limit 3e7)

  (let [r (range limit)]
    [(last r) (first r)])

  (let [r (eduction (range limit))]
    [(last r) (first r)]))

;; A custom stateful transducer that drops every nth element
;; from the input:

(defn drop-nth [n]
  (fn [rf]
    (let [nv (volatile! -1)]
      (fn
        ([] (rf))
        ([result] (rf result))
        ([result input]
         (let [i (vswap! nv inc)]
           (if (zero? (rem i n))
             result
             (rf result input))))))))

;; Using the new transducer:

(transduce (drop-nth 2) conj [1 2 3 4 5 6 7 8])
(transduce (take-nth 2) conj [1 2 3 4 5 6 7 8])

;; An alternative implementation using an atom for local state:

(defn drop-nth-atom [n]
  (fn [rf]
    (let [nv (atom -1)]
      (fn
        ([] (rf))
        ([result] (rf result))
        ([result input]
         (let [i (swap! nv inc)]
           (if (zero? (rem i n))
             result
             (rf result input))))))))

;; Benchmarking the two alternatives:

(comment
  (quick-bench (into [] (drop-nth 2) (range 1e3)))

  (quick-bench (into [] (drop-nth-atom 2) (range 1e3))))

;; A custom stateful transducer that holds back all inputs of type string
;; and produces those strings once all non-string input has been processed:

(defn strings-to-the-back [rf]
  (let [stringsv (volatile! (java.util.ArrayList.))]
    (fn
      ([] (rf))
      ([result]
       (let [^java.util.ArrayList strings @stringsv
             result (if (.isEmpty strings)
                      result
                      (let [v (vec strings)]
                        (.clear strings)
                        (vreset! stringsv strings)
                        (reduce rf result v)))]
         (rf result)))
      ([result input]
       (let [^java.util.ArrayList strings @stringsv]
         (if (string? input)
           (do
             (.add strings input)
             (vreset! stringsv strings)
             result)
           (rf result input)))))))

;; A utility function to produce lazy seqs consisting of
;; strings and integers:

(defn strings-and-numbers [n]
  (->> (range n)
       (map (fn [x] (if (<= (rand) 0.5) (str x) x)))))

;; Using the new transducer:

(into [] strings-to-the-back (strings-and-numbers 10))

(let [c (chan 3 strings-to-the-back)]
  (>!! c 1)
  (>!! c "2")
  (>!! c 3)
  (close! c)
  (-> []
      (conj (<!! c))
      (conj (<!! c))
      (conj (<!! c))))

;; The stateful transducers partition-all and partition-by construct a
;; vector from an ArrayList l as follows: (vec (.toArray l)).
;; The benchmarks below show that this conversion is faster than
;; simply doing (vec l) for small lists.

(defn array-list [n]
  (java.util.ArrayList. ^java.util.Collection (range n)))

(comment
  (def n 10)
  (quick-bench (vec (array-list n)))
  (quick-bench (vec (.toArray ^java.util.ArrayList (array-list n)))))

;; A function to demonstrate the reuse of a transducer:

(defn transduce-all
  ([xform f init & colls]
   (let [f (xform f)]
     (for [coll colls
           :let [ret (if (instance? clojure.lang.IReduceInit coll)
                       (.reduce ^clojure.lang.IReduceInit coll f init)
                       (protocols/coll-reduce coll f init))]]
       (f ret)))))

(transduce-all strings-to-the-back conj [] ["1" 2 "3"] [4 "5" 6] ["7" "8" 9])

;; Some reducing function will terminate early instead of processing the entire input.
;; This reduction function will terminate after processing a single value:

(defn highlander-rf
  "There can be only one"
  [_ input] (reduced input))

(reduce highlander-rf nil [1 2 3 4])

;; This transducer will also stop processing items after the first one:

(defn highlander-tr
  "There can be only one"
  [rf]
  (fn
    ([] (rf))
    ([result] (rf result))
    ([result input] (ensure-reduced (rf result input)))))

(into []
      highlander-tr
      [1 2 3 4])

;; The following transducer outputs each value it receives as input twice,
;; but it is broken.

(defn broken-stutter [rf]
  (fn
    ([] (rf))
    ([result] (rf result))
    ([result input]
     (rf (rf result input) input))))

(transduce
 broken-stutter
 conj
 []
 [1 2 3 4 5 6])

;; Using the transducer above with the following reduction function,
;; which terminates early, shows that the transducer is broken.

(defn limited-conj [n]
  (fn 
    ([result] result)
    ([result input]
     (if (>= (count result) n)
       (reduced result)
       (conj result input)))))

(transduce
 broken-stutter
 (limited-conj 6)
 []
 [1 2 3 4 5 6])

;; The following version of stutter is not broken. It does not call
;; the reducing function once this function has returned a
;; reduced value.

(defn stutter [rf]
  (fn
    ([] (rf))
    ([result] (rf result))
    ([result input]
     (let [intermediate (rf result input)]
       (if (reduced? intermediate)
         intermediate
         (rf intermediate input))))))

(transduce
 stutter
 (limited-conj 6)
 []
 [1 2 3 4 5 6])

;; Another reducing function that terminates early, taken from
;; https://clojuredocs.org/clojure.core/unreduced#example-64458dd4e4b08cf8563f4b96:

(defn conj-till-odd
  ([coll] coll)
  ([coll x] (cond-> (conj coll x)
              (odd? x) reduced)))

;; A transducer that adds the last processed input value to the output:

(defn repeat-last [rf]
  (let [pv (volatile! nil)]
    (fn
      ([] (rf))
      ([result]
       (if-let [p @pv]
         (unreduced (rf result p))
         (rf result)))
      ([result input]
       (let [result (rf result input)]
         (vreset! pv input)
         result)))))

;; Evaluate the following expression with and without the call to unreduced
;; in repeat-last.

(transduce
 repeat-last
 conj-till-odd
 []
 [2 4 3 2])
