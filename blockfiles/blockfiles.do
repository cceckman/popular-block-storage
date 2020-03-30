
redo-ifchange blockfiles.go

exec 1>&2

go get .
go build -o "$3"