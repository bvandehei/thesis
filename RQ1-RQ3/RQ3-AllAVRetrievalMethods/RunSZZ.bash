#!/bin/bash

FILES=CreateInputFilesForSZZ/IssueJsonFiles/*
for f in $FILES
do
  n=${f%_issue*}
  n=${n##*/}
  for i in 1
  do
    newname="FixAndIntroducingCommitsFiles/"
    newname+="$n"
    newname+="_fix_and_introducers_pairs_"
    newname+="$i.json"
    repo="CreateInputFilesForSZZ/GitClones/"
    repo+="$n/"
    echo "java -jar szz_find_bug_introducers-0.1.jar -i $f -r $repo -d $i"
    java -jar szz_find_bug_introducers-0.1.jar -i $f -r $repo -d $i
    mv results/fix_and_introducers_pairs.json $newname
    rm -rf issues/
    rm -rf results/
  done
done
