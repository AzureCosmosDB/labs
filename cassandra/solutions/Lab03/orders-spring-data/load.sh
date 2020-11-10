while true
do
    curl -X POST -H "Content-Type: application/json" -d '{"amount":"42", "location":"foo"}' http://localhost:9090/orders

    sleep 5s
done
