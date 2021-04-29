(ns motform.strange.material.styles
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
   :selection  "hsb(0, 0%, 95%)"
   :transparent "hsba(0, 0%, 0%, 0.0)"})

(def styles
  (css/register
   ::style
   (let [padding 10]
     {::padding padding

      ".root" {:-fx-background-color (colors :background)
               :-fx-font-size        20}

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

      ".text-area" {:-fx-background-color "red"}

      ".kernel"
      {"-command-line" {:-fx-padding    [0 0 0 0]
                        :-fx-background (colors :frame)}

       "-command-history" {:-fx-padding 0
                           "-tab" {:-fx-text-fill        (colors :foreground)
                                   :-fx-padding          [5 10 2 10]
                                   :-fx-border-width     [1 1 0 0]
                                   :-fx-font-size        16
                                   :-fx-border-color     (colors :frame)
                                   :-fx-font-family      "Inconsolata"
                                   ":hover" {:-fx-background-color (colors :frame)
                                             :-fx-text-fill        (colors :background)}
                                   "-selected" {:-fx-background-color (colors :frame)
                                                :-fx-text-fill        (colors :background)}}}

       "-sidebar" {:-fx-font-size    12
                   :-fx-border-color (colors :frame)
                   :-fx-border-width [1 0 0 0]

                   "-container" {"-label"
                                 {:-fx-padding          [5 5 5 10]
                                  :-fx-font-size        12
                                  :-fx-background-color (colors :frame)
                                  "-text" {:-fx-text-fill (colors :background)}}}

                   "-list" {:-fx-background-color (colors :sidebar)
                            :-fx-border-width     0
                            :-fx-padding [10 0 10 0]}}

       "-system-call-item" {:-fx-background-color (colors :sidebar)
                            :-fx-padding          [2 0 2 10]
                            :-fx-font-size        14
                            :-fx-font-family      "Inconsolata"
                            ":hover"    {:-fx-background-color (colors :highlight)}
                            "-selected" {:-fx-background-color (colors :highlight)
                                         :-fx-padding          [2 0 2 10]
                                         :-fx-font-size        14
                                         :-fx-font-family      "Inconsolata"}}

       "-std-out-item" {:-fx-background-color (colors :sidebar)
                        :-fx-padding          [2 0 2 10]
                        :-fx-font-size        14
                        :-fx-font-family      "Inconsolata"}

       "-email" {:-fx-background-color "transparent"
                 :-fx-padding          0
                 :-fx-border-color     (colors :frame)
                 :-fx-border-width     [1 0 1 0]

                 "-view" {:-fx-padding      10
                          :-fx-border-width [0 0 1 0]
                          :-fx-border-color (colors :border)
                          :-fx-font-size    15
                          ":hover"    {:-fx-background-color (colors :selection)}

                          "-topic"    {:-fx-padding      [0 0 5 0]}

                          "-selected" {:-fx-background-color (colors :selection)}

                          "-meta"     {:-fx-text-fill (colors :meta)
                                       :-fx-font-size 11}

                          "-body"     {:-fx-padding     0
                                       :-fx-font-family "Inconsolata"
                                       :-fx-background-color (colors :selection)}

                          "-offset-button" {:-fx-alignment "CENTER"
                                            :-fx-text-fill "red"
                                            :-fx-font-size 10}}}}})))
