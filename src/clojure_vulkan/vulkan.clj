(ns clojure-vulkan.vulkan
  (:require [clojure-vulkan.instance :as instance]))

(defn init []
  (instance/create))

(defn terminate []
  (instance/destroy))