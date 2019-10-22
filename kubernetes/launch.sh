#!/bin/bash
kubectl apply -f wcfc-quiz.yaml
export NODE_PORT=$(kubectl get services/wcfc-quiz -o go-template='{{(index .spec.ports 0).nodePort}}')
echo http://$(minikube ip):$NODE_PORT
