(ns clojure-vulkan.uniform)

(deftype UniformButterObject [^:unsynchronized-mutable model
                              ^:unsynchronized-mutable view
                              ^:unsynchronized-mutable projection])