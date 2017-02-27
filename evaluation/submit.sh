#!/usr/bin/env bash
java -jar target/scala-2.11/soar-eval.jar -i $1 -o $2 --modules "CSC3621","CSC3222","CSC2026","CSC2024" --common "CSC2024"