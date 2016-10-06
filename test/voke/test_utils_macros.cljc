(ns voke.test-utils-macros)

(defmacro check
  "Meant to be used in a (cljs.test/deftest). Invokes cljs.spec.test/check on var and
  verifies that there weren't any failures in the generated tests."
  ([var]
   `(check ~var 1000))
  ([var num-tests]
   `(do
      (cljs.test/is (= (cljs.spec.test/summarize-results
                         (cljs.spec.test/check ~var
                                               {:clojure.test.check/opts {:num-tests ~num-tests}}))
                       {:total 1 :check-passed 1}))
      (cljs.spec.test/unstrument ~var))))
