(ns clojure-vulkan.validation-layers
  (:require [clojure-vulkan.util :as util]
            [clojure.string :as str])
  (:import (org.lwjgl.vulkan VK13 VkLayerProperties$Buffer VkLayerProperties)
           (org.lwjgl.system MemoryStack)))

(def ^:dynamic *validation-layers* #{"VK_LAYER_KHRONOS_validation"})

(defn check-validation-layers-support []
  (util/with-memory-stack ^MemoryStack stack
    (let [layer-count (.ints stack 0)
          _ (VK13/vkEnumerateInstanceLayerProperties layer-count nil)
          ^VkLayerProperties$Buffer available-layers (VkLayerProperties/malloc (.get layer-count 0) stack)
          _ (VK13/vkEnumerateInstanceLayerProperties layer-count available-layers)
          available-layers (->> available-layers .stream .iterator iterator-seq
                                (map (memfn ^VkLayerProperties layerNameString)) set)]
      (when-not (every? #(contains? available-layers %) *validation-layers*)
        (throw (RuntimeException. (str "Some of validation layers are not supported."
                                       "\nAvailable layers: " (str/join ", " available-layers)
                                       "\nRequired layers: " (str/join ", " *validation-layers*))))))))