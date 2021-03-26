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
       :-fx-text-fill        (colors :foreground)
       :-fx-font-size        20}

      ".sidebar"
      {:-fx-background-color (colors :sidebar)}

      ".sidebar-panel-indicator"
      {:-fx-text-fill (colors :inactive)
       :-fx-padding   [7 15 7 15]
       ":hover"
       {:-fx-text-fill        (colors :foreground)
        :-fx-background-color (colors :border)}}

      ".sidebar-panel-indicator-active"
      {:-fx-text-fill        (colors :foreground)
       :-fx-background-color (colors :background)
       ":hover"
       {:-fx-background-color (colors :background)}}

      ".panel"
      {:-fx-padding [8 20 20 20]
       :-fx-spacing 20}

      ".timeline"
      {}

      ".repl"
      {:-fx-font-family "Inconsolata"
       :-fx-padding [20 0 0 0]
       :-fx-font-size   20}

      ".repl-submit"
      {:-fx-padding           10
       :-fx-border-width      1
       :-fx-font-size         15
       :-fx-background-color  (colors :sidebar)}
      })))

