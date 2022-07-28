(ns clojure-vulkan.vulkan
  (:require [clojure-vulkan.debug :as debug]
            [clojure-vulkan.instance :as instance]
            [clojure-vulkan.validation-layers :as validation-layers]
            [clojure-vulkan.physical-device :as physical-device]))

(defn init []
  (instance/create)
  (debug/setup-debug-messenger)
  (physical-device/pick-physical-device))

(defn terminate []
  (when validation-layers/*enable-validation-layers*
    (debug/destroy-debug-messenger nil))
  (instance/destroy))