(ns clojure-vulkan.vulkan
  (:require [clojure-vulkan.debug :as debug]
            [clojure-vulkan.instance :as instance]
            [clojure-vulkan.validation-layers :as validation-layers]))

(defn init []
  (instance/create)
  (debug/setup-debug-messenger))

(defn terminate []
  (when validation-layers/*enable-validation-layers*
    (debug/destroy-debug-messenger nil))
  (instance/destroy))