locations=(NY LA Chicago Seattle NJ Austin Boston SanJose)
for loc in "${locations[@]}"
    do
        size=${#locations[@]}
        index=$(($RANDOM % $size))
        amount=$((50 + RANDOM % 450))
        curl -s -d '{"amount":"'$amount'", "location":"'$loc'"}' -H "Content-Type: application/json" -X POST http://localhost:8080/orders
        sleep 3s
    done


