(ns org.motform.strange-materials.lkml.styles
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
   {".root" {:-fx-background-color (colors :background)
             :-fx-font-family       "Inconsolata"
             :-fx-font-size        20}

    ".scroll-bar:vertical"   {:-fx-pref-width  10}
    ".scroll-bar:horizontal" {:-fx-pref-height 10}

    ".track" {:-fx-background-color (colors :background)}
    ".thumb" {:-fx-background-color (colors :background) :-fx-background-radius 5}

    ".increment-button" {:-fx-background-color (colors :background) :-fx-padding 0   :-fx-border-color (colors :background)}
    ".decrement-button" {:-fx-background-color (colors :background) :-fx-padding -50 :-fx-border-color (colors :background)}
    ".decrement-arrow"  {:-fx-padding 0 :-fx-border-width 0 :-fx-border-color (colors :background) :-fx-shape " "}
    ".increment-arrow"  {:-fx-padding 0 :-fx-border-width 0 :-fx-border-color (colors :background) :-fx-shape " "}
    ".scroll-arrow"     {:-fx-padding 0}

    ".text-field" {:-fx-background-color  (colors :background)
                   :-fx-text-fill         (colors :foreground)
                   :-fx-padding           [15 15 15 15]
                   :-fx-font-family       "Inconsolata"
                   :-fx-font-size         25
                   :-fx-background-radius 0}

    ".text-area" {:-fx-background-color "red"}

    ".command-line" {:-fx-padding    [0 0 0 0]
                     :-fx-background (colors :frame)}

    ".command-history" {:-fx-padding 10
                        :-fx-spacing 10
                        "-tab" {:-fx-padding          [5 10]
                                :-fx-border-width     1
                                :-fx-border-color     (colors :frame)
                                :-fx-border-radius    12
                                :-fx-font-size        16
                                :-fx-font-family      "Inconsolata"
                                ":hover" {:-fx-background-color (colors :sidebar)
                                          :-fx-background-radius 12}
                                "-selected" {:-fx-background-color (colors :sidebar)
                                             :-fx-background-radius 12}}}

    ".sidebar" {:-fx-border-color (colors :foreground)
                :-fx-border-width [1 0 0 0]

                "-container" {"-label"
                              {:-fx-padding   [20 5 5 15]
                               :-fx-font-size 15
                               :-fx-font-weight 700
                               "-text" {:-fx-text-fill (colors :foreground)}}}

                "-list" {:-fx-padding 10
                         :-fx-spacing 5
                         "-sublist" {:-fx-spacing 5}}}

    ".system-call-item" {:-fx-padding            [3 0 3 10]
                         :-fx-border-width       1
                         :-fx-border-color       (colors :frame)
                         :-fx-border-radius      10
                         :-fx-background-radius  10
                         :-fx-font-size          14
                         :-fx-font-family        "Inconsolata"
                         ":hover"    {:-fx-background-color  (colors :sidebar)}
                         "-selected" {:-fx-background-color  (colors :sidebar)
                                      :-fx-padding           [3 0 3 10]
                                      :-fx-font-size         14
                                      :-fx-border-color      (colors :frame)
                                      :-fx-border-width      1
                                      :-fx-background-radius 10
                                      :-fx-border-radius     10
                                      :-fx-font-family       "Inconsolata"}}

    ".std-out" {"-container" {}
                "-item" {:-fx-padding [2 0 2 5]
                         "-text" {:-fx-font-size   14
                                  :-fx-font-family "Inconsolata"}}}

    ".email" {:-fx-background-color "transparent"
              :-fx-font-family      "Inconsolata"
              "-container" {:-fx-border-color (colors :frame)
                            :-fx-border-width [1 0 0 0]}
              "-list" {:-fx-spacing 5
                       :-fx-padding [10 0 0 10]
                       "-more" {:-fx-padding           8
                                :-fx-border-radius     10
                                :-fx-background-radius 10
                                :-fx-border-color      (colors :frame)
                                :-fx-font-size         15
                                ":hover" {:-fx-background-color (colors :sidebar)}
                                "-container" {:-fx-padding [20 10]}}}
              "-view" {:-fx-padding           10
                       :-fx-border-width      1
                       :-fx-border-radius     10
                       :-fx-background-radius 10
                       :-fx-border-color      (colors :frame)
                       :-fx-font-size         15
                       ":hover"    {:-fx-background-color (colors :selection)}
                       "-topic"    {:-fx-padding [0 0 5 0]}
                       "-selected" {:-fx-background-color (colors :selection)}
                       "-meta"     {:-fx-text-fill (colors :foreground)
                                    :-fx-font-size 11}
                       "-body"     {:-fx-padding     [10 0 0 10]
                                    :-fx-font-size   15
                                    :-fx-font-family "Inconsolata"}
                       "-offset-button" {:-fx-alignment "CENTER"
                                         :-fx-text-fill "red"
                                         :-fx-font-size 10}}}}))
