((clojure-mode . ((eval . (define-key evil-normal-state-map (kbd "ö")
                            '(lambda ()
                               (interactive)
                               (cider-interactive-eval "(swap! org.motform.strange-materials.aai.server.ui/*state identity)")))))))
