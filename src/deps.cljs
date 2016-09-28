{:foreign-libs [{:file        "resources/public/js/rbush.js"
                 :provides    ["rbush"]
                 :module-type :commonjs}
                {:file        "resources/public/js/collision.js"
                 :requires    ["rbush"]
                 :provides    ["collision"]
                 :module-type :commonjs}]}
