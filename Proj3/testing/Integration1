# Tests that find will find any commit we throw at it (if it exists).
> init
<<<
D HASH "[a-f0-9]+"
+ e.txt eumaeus.txt
> add e.txt
<<<
> commit "Added eumaeus"
<<<
> find "Added eumaeus"
${HASH}
<<<*
> find "A"
Found no commit with that message.
<<<
+ u.txt eumaeus.txt
> add u.txt
<<<
> commit "Added eumaeus"
<<<
> find "Added eumaeus"
${HASH}
${HASH}
<<<*