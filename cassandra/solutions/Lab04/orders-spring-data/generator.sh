locations=(NY LA Chicago Seattle NJ Austin Boston SanJose)
while true;
    do
        size=${#locations[@]}
        index=$(($RANDOM % $size))
        amount=$((50 + RANDOM % 450))
        loc=${locations[$index]}
        curl -s -d '{"amount":"'$amount'", "location":"'$loc'"}' -H "Content-Type: application/json" -X POST http://localhost:8080/orders
        sleep 3s
done


