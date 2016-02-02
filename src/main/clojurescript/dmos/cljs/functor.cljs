(ns dmos.cljs.functor)

(defmulti fmap
  "Applies function f to each item in the data structure s and returns
  a structure of the same kind."
  {:arglists '([f s])}
  (fn [f s] (type s)))

(defmethod fmap cljs.core/List
  [f v]
  (map f v))

(defmethod fmap cljs.core/PersistentVector
  [f v]
  (into (empty v) (map f v)))

(defn- map-fmap
  [f m]
  (into (empty m) (for [[k v] m] [k (f v)])))

(defmethod fmap cljs.core/PersistentHashMap
  [f m]
  (map-fmap f m))

(defmethod fmap cljs.core/PersistentTreeMap
  [f m]
  (map-fmap f m))

(defmethod fmap cljs.core/PersistentArrayMap
  [f m]
  (map-fmap f m))

(defn- set-fmap
  [f s]
  (into (empty s) (map f s)))

(defmethod fmap cljs.core/PersistentHashSet
  [f s]
  (set-fmap f s))

(defmethod fmap cljs.core/PersistentTreeSet
  [f s]
  (set-fmap f s))
