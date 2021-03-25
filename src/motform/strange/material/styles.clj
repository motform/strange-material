(ns motform.strange.material.styles
  (:require [cljfx.css :as css]))

(def ^:private colors
  {:background "hsb(0, 0%, 100%)"
   :foreground "hsb(0, 0%, 0%)"
   :inactive   "hsb(0, 0%, 50%)"
   :sidebar    "hsb(0, 0%, 90%)"
   :border     "hsb(0, 0%, 80%)"})

(def styles
  (css/register
   ::style
   (let [padding 10]
     {::padding padding

      "@font-face"
      {:src "url(\"resources/fonts/MDSystemTest-Regular.otf\")"}

      ".root"
      {:-fx-background-color (colors :background)
       ;; :-fx-font-family      "MD IO 0.2 1.4"
       :-fx-font-size        20}

      ".sidebar"
      {:-fx-background-color (colors :sidebar)}

      ".sidebar-panel-indicator"
      {:-fx-text-fill (colors :inactive)
       :-fx-padding   [7 15 7 15]
       ":hover"
       {:-fx-text-fill (colors :foreground)}}

      ".panel"
      {:-fx-padding [8 20 20 20]}

      ".sidebar-panel-indicator-active"
      {:-fx-text-fill (colors :foreground)
       :-fx-background-color (colors :background)}

      ".timeline"
      {}

      ".repl"
      {:-fx-font-family "Menlo"
       :-fx-padding [20 0 0 0]
       :-fx-font-size   15}

      })))

