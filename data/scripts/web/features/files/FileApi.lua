local FileController = require("web.features.files.FileController")

local FileApi = {}

local routes = {
    get = {
        ["/api/files"] = "list",
        ["/api/files/files"] = "listFiles",
        ["/api/files/directories"] = "listDirectories",
        ["/api/file"] = "get",
        ["/api/file/info"] = "info",
        ["/api/file/download"] = "download",
        ["/api/file/exists"] = "exists",
        ["/api/file/permissions"] = "permissions",
        ["/api/file/space"] = "space",
        ["/api/file/paths"] = "paths"
    },

    post = {
        ["/api/file"] = "create",
        ["/api/file/upload"] = "upload",
        ["/api/file/copy"] = "copy",
        ["/api/file/move"] = "move",
        ["/api/file/rename"] = "rename",
        ["/api/file/mkdir"] = "createDirectory",
        ["/api/file/mkdirs"] = "createDirectories",
        ["/api/file/link"] = "link",
        ["/api/file/zip"] = "zip",
        ["/api/file/unzip"] = "unzip",
        ["/api/file/modified"] = "setLastModified",
        ["/api/file/size"] = "setSize",
        ["/api/file/writable"] = "setWriteable",
        ["/api/file/readonly"] = "setReadOnly",
        ["/api/file/executable"] = "setExecutable"
    },

    put = {
        ["/api/file"] = "save"
    },

    delete = {
        ["/api/file"] = "delete"
    }
}

local function handle(method, request)
    local controllerMethod = routes[method][request.path]

    if not controllerMethod then
        return
    end

    return FileController[controllerMethod](FileController, request)
end

function FileApi:get(request)
    return handle("get", request)
end

function FileApi:post(request)
    return handle("post", request)
end

function FileApi:put(request)
    return handle("put", request)
end

function FileApi:delete(request)
    return handle("delete", request)
end

return FileApi
