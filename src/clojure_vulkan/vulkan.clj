(ns clojure-vulkan.vulkan
  (:require [clojure-vulkan.instance :as instance]
            [clojure-vulkan.debug :as debug]))

(defn init []
  (instance/create)
  (debug/setup-debug-messenger))

(defn terminate []
  (instance/destroy))