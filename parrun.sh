
COMMAND=$1
NRUNS=$2

for i in `seq 1 $NRUNS`
do
    echo "run $i"
    $COMMAND & disown
done

