(ns clojure-vulkan.vulkan
  (:require [clojure-vulkan.debug :as debug]
            [clojure-vulkan.globals :as globals]
            [clojure-vulkan.instance :as instance]
            [clojure-vulkan.logical-device-and-queue :as logical-device-and-queue]
            [clojure-vulkan.physical-device :as physical-device]
            [clojure-vulkan.swap-chain :as swap-chain]
            [clojure-vulkan.validation-layers :as validation-layers]
            [clojure-vulkan.window-surface :as window-surface]))

(defn init []
  (instance/create)
  (debug/setup-debug-messenger)
  (window-surface/create-surface)
  (physical-device/pick-physical-device)
  (logical-device-and-queue/create-logical-device)
  (swap-chain/create-swap-chain))

(defn terminate []
  (when validation-layers/*enable-validation-layers*
    (debug/destroy-debug-messenger nil))
  (swap-chain/destroy-swapchain)
  (window-surface/destroy-surface)
  (logical-device-and-queue/destroy-logical-device)
  (instance/destroy)
  (globals/reset-queue-families))