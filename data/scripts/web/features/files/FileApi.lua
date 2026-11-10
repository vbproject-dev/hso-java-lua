local FileController = require("web.features.files.FileController")

local FileApi = {}

local routes = {
    get = {
        ["/lua/files"] = "list",
        ["/lua/files/files"] = "listFiles",
        ["/lua/files/directories"] = "listDirectories",
        ["/lua/file"] = "get",
        ["/lua/file/info"] = "info",
        ["/lua/file/download"] = "download",
        ["/lua/file/exists"] = "exists",
        ["/lua/file/permissions"] = "permissions",
        ["/lua/file/space"] = "space",
        ["/lua/file/paths"] = "paths"
    },

    post = {
        ["/lua/file"] = "create",
        ["/lua/file/upload"] = "upload",
        ["/lua/file/copy"] = "copy",
        ["/lua/file/move"] = "move",
        ["/lua/file/rename"] = "rename",
        ["/lua/file/mkdir"] = "createDirectory",
        ["/lua/file/mkdirs"] = "createDirectories",
        ["/lua/file/link"] = "link",
        ["/lua/file/zip"] = "zip",
        ["/lua/file/unzip"] = "unzip",
        ["/lua/file/modified"] = "setLastModified",
        ["/lua/file/size"] = "setSize",
        ["/lua/file/writable"] = "setWriteable",
        ["/lua/file/readonly"] = "setReadOnly",
        ["/lua/file/executable"] = "setExecutable"
    },

    put = {
        ["/lua/file"] = "save"
    },

    delete = {
        ["/lua/file"] = "delete"
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
