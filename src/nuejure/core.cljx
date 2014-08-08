(ns nuejure.core
  (:require
   [nuejure.functor :as f]
   [nuejure.applicative :as a]
   [nuejure.monad :as m :include-macros true]
   [nuejure.foldable :as l]
   [nuejure.traversable :as t]))

(defn update
  ([m k f] (assoc m k (f (get m k))))
  ([m k f x1] (assoc m k (f (get m k) x1)))
  ([m k f x1 x2] (assoc m k (f (get m k) x1 x2)))
  ([m k f x1 x2 & xs] (assoc m k (apply f (get m k) x1 x2 xs))))

(def return a/return)

(defn mapf
  ([f a] (f/mapf f a))
  ([f a & as] (apply a/ap (f/mapf (partial partial f) a) as)))

(def ap a/ap)

(def bind m/bind)

(def fold l/fold)

(def traverse t/traverse)

(defn compm [f & fs]
  (let [[f & fs] (reverse (cons f fs))]
    (fn [a] (apply bind (f a) fs)))) 

(defn mdo [& ms]
  (reduce #(mapf (fn [_ a] a) % %2) (return nil) ms))

(defmacro mlet [bindings & body]
  (if (and (vector? bindings) (even? (count bindings)))
    (if (seq bindings)
      (let [[sym val] bindings
            cont `(mlet ~(subvec bindings 2) ~@body)]
        `(bind ~val (fn [~sym] ~cont)))
      `(mdo ~@body))
    (throw (IllegalArgumentException.
            "bindings has to be a vector with even number of elements."))))

(defn- threadf [lift f & transforms]
  `(->> ~f ~@(map lift transforms)))

(defmacro f->> [& body]
  (apply threadf (fn [t] `(mapf (partial ~@t))) body))

(defmacro f-> [& body]
  (apply threadf (fn [[f & args]] `(mapf #(~f % ~@args))) body))
