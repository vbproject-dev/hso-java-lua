--[[
--
--Filename: FileService.lua
--
--Created Date: Thursday, October 1st 2026, 8:06:22 am
--
--Author: VBPROJECT
--
--]]

local FileService = {}

function FileService:new(path)
    return File.new(path)
end

function FileService:path(path)
    return File.path(path)
end

function FileService:absolutePath(path)
    return File.absolutePath(path)
end

function FileService:executablePath(path)
    return File.executablePath(path)
end

function FileService:exists(path)
    return File.exists(path)
end

function FileService:existsAnywhere(path)
    return File.existsAnywhere(path)
end

function FileService:canRead(path)
    return File.canRead(path)
end

function FileService:canWrite(path)
    return File.canWrite(path)
end

function FileService:canExecute(path)
    return File.canExecute(path)
end

function FileService:isFile(path)
    return File.isFile(path)
end

function FileService:isDirectory(path)
    return File.isDirectory(path)
end

function FileService:isLink(path)
    return File.isLink(path)
end

function FileService:isDevice(path)
    return File.isDevice(path)
end

function FileService:isHidden(path)
    return File.isHidden(path)
end

function FileService:created(path)
    return File.created(path)
end

function FileService:lastModified(path)
    return File.lastModified(path)
end

function FileService:size(path)
    return File.size(path)
end

function FileService:setLastModified(path, timestamp)
    return File.setLastModified(path, timestamp)
end

function FileService:setSize(path, size)
    return File.setSize(path, size)
end

function FileService:setWriteable(path, value)
    return File.setWriteable(path, value)
end

function FileService:setReadOnly(path, value)
    return File.setReadOnly(path, value)
end

function FileService:setExecutable(path, value)
    return File.setExecutable(path, value)
end

function FileService:info(path)
    return File.info(path)
end

function FileService:copy(source, destination, options)
    return File.copy(source, destination, options or File.COPY_DEFAULT)
end

function FileService:move(source, destination, options)
    return File.move(source, destination, options or File.COPY_DEFAULT)
end

function FileService:rename(source, destination, options)
    return File.rename(source, destination, options or File.COPY_DEFAULT)
end

function FileService:link(source, destination, type)
    return File.link(
        source,
        destination,
        type or File.LINK_HARD
    )
end

function FileService:remove(path, recursive)
    return File.remove(path, recursive or false)
end

function FileService:createFile(path)
    return File.createFile(path)
end

function FileService:createDirectory(path)
    return File.createDirectory(path)
end

function FileService:createDirectories(path)
    return File.createDirectories(path)
end

function FileService:read(path)
    return File.read(path)
end

function FileService:write(path, content)
    return File.write(path, content or "")
end

function FileService:list(path)
    local files, err = File.list(path)

    if not files then
        return nil, err
    end

    local result = {}

    for _, path in ipairs(files) do
        local info, err = File.info(path)

        if not info then
            return nil, err
        end

        result[#result + 1] = info
    end

    return result
end

function FileService:listFiles(path)
    local files, err = File.listFiles(path)

    if not files then
        return nil, err
    end

    local result = {}

    for _, path in ipairs(files) do
        local info, err = File.info(path)

        if not info then
            return nil, err
        end

        result[#result + 1] = info
    end

    return result
end

function FileService:listDirectories(path)
    local directories, err = File.listDirectories(path)

    if not directories then
        return nil, err
    end

    local result = {}

    for _, path in ipairs(directories) do
        local info, err = File.info(path)

        if not info then
            return nil, err
        end

        result[#result + 1] = info
    end

    return result
end

function FileService:totalSpace(path)
    return File.totalSpace(path)
end

function FileService:usableSpace(path)
    return File.usableSpace(path)
end

function FileService:freeSpace(path)
    return File.freeSpace(path)
end

function FileService:getExtension(path)
    local ext = path:match("%.([^%.]+)$")
    return ext and ext:lower()
end

function FileService:zip(source, destination)
    return File.zip(source, destination)
end

function FileService:unzip(source, destination)
    return File.unzip(source, destination)
end

return FileService
