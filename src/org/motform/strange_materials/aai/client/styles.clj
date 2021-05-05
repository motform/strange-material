(ns org.motform.strange-materials.aai.client.styles
  (:require [cljfx.css :as css]))

(def ^:private colors
  {:background "hsb(0, 0%, 100%)"
   :scroll-bar "hsb(0, 0%, 100%)"
   :sidebar    "hsb(0, 0%, 90%)"
   :highlight  "hsb(0, 0%, 70%)"
   :frame      "hsb(0, 0%, 20%)"

   :inactive   "hsb(0, 0%, 50%)"
   :meta       "hsb(0, 0%, 30%)"
   :foreground "hsb(0, 0%, 0%)"

   :border     "hsb(0, 0%, 80%)"
   :selection  "hsb(0, 0%, 98%)"
   :transparent "hsba(0, 0%, 0%, 0.0)"})

(def styles
  (css/register
   ::style
   (let [padding 10]
     {::padding padding

      "@font-face" {:-fx-font-family "MD System"
                    :src "url(\"resources/fonts/MDSystemTest-Regular.ttf\")"}

      ".root" {:-fx-background-color (colors :background)
               :-fx-font-family      "MD System"
               }

      ".scroll-bar:vertical"   {:-fx-pref-width  10}
      ".scroll-bar:horizontal" {:-fx-pref-height 10}

      ".track" {:-fx-background-color (colors :frame)}
      ".thumb" {:-fx-background-color (colors :scroll-bar) :-fx-background-radius 0}

      ".increment-button" {:-fx-background-color (colors :frame) :-fx-padding 0   :-fx-border-color (colors :frame) }
      ".decrement-button" {:-fx-background-color (colors :frame) :-fx-padding -50 :-fx-border-color (colors :frame)}
      ".decrement-arrow"  {:-fx-padding 0 :-fx-border-width 0 :-fx-border-color (colors :frame) :-fx-shape " "}
      ".increment-arrow"  {:-fx-padding 0 :-fx-border-width 0 :-fx-border-color (colors :frame) :-fx-shape " "}
      ".scroll-arrow"     {:-fx-padding 0}

      ".text-field" {:-fx-background-color  (colors :background)
                     :-fx-text-fill         (colors :foreground)
                     :-fx-padding           [15 15 15 15]
                     :-fx-font-family      "Inconsolata"
                     :-fx-font-size        25
                     :-fx-background-radius 0}

      ".pane" {:-fx-font-size 18}

      ".quota" {"-container" {:-fx-padding 20}}

      ".chat" {"-container" {:-fx-padding 0}

               "-message" {:-fx-padding 5 
                           :-fx-border-radius 5
                           ":last-visible" {:-fx-padding 50}
                           "-container"    {:-fx-padding [10 20]}
                           "-author"       {:-fx-text-fill (colors :border)
                                            :-fx-padding   [0 0 10 0]}}

               "-input" {"-container" {:-fx-border-width     [1 0]
                                       :-fx-border-color     (colors :border)
                                       :-fx-background-color (colors :selection)
                                       :-fx-padding          [15 10 15 20]}}}
      })))
