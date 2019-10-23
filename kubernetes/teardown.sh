#!/bin/bash
kubectl delete service wcfc-quiz
kubectl delete deployment wcfc-quiz
kubectl delete service mongodb
kubectl delete sts mongodb
