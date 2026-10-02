--[[
--
--Filename: ModuleRegistry.lua
--
--Created Date: Thursday, October 1st 2026, 8:06:48 am
--
--Author: VBPROJECT
--
--]]

local ModuleRegistry = {
    modules = {}
}

local function toModuleName(path)
    return path
        :gsub("\\", "/")
        :gsub("%.lua$", "")
        :gsub("/", ".")
end

local function toDirectoryPath(name)
    return name:gsub("%.", "/")
end

function ModuleRegistry.loadModule(name)
    local module = require(name)

    ModuleRegistry.modules[name] = module

    return module
end

function ModuleRegistry.loadPackage(prefix)
    local files = FileUtils.listFiles(toDirectoryPath(prefix), true)

    for _, file in ipairs(files) do
        if file:sub(-4) == ".lua" then
            ModuleRegistry.loadModule(
                prefix .. "." .. toModuleName(file)
            )
        end
    end
end

function ModuleRegistry.get(name)
    return ModuleRegistry.modules[name] or ModuleRegistry.loadModule(name)
end

function ModuleRegistry.has(name)
    return ModuleRegistry.modules[name] ~= nil
end

function ModuleRegistry.clear()
    ModuleRegistry.modules = {}
end

local function reloadModule(name)
    local old = ModuleRegistry.modules[name]

    package.loaded[name] = nil

    local success, new = xpcall(function()
        return require(name)
    end, debug.traceback)

    if not success then
        package.loaded[name] = old
        ModuleRegistry.modules[name] = old

        log("[ModuleRegistry] Reload failed: %s\n%s", name, new)

        return false
    end

    -- Keep the original module table alive.
    if type(old) == "table" and type(new) == "table" then
        for key in pairs(old) do
            old[key] = nil
        end

        for key, value in pairs(new) do
            old[key] = value
        end

        package.loaded[name] = old
        ModuleRegistry.modules[name] = old
    else
        ModuleRegistry.modules[name] = new
    end

    return true
end

function ModuleRegistry.reload()
    local modules = {}

    for name in pairs(ModuleRegistry.modules) do
        modules[#modules + 1] = name
    end

    table.sort(modules)

    for _, name in ipairs(modules) do
        if not reloadModule(name) then
            return false
        end
    end

    log("[ModuleRegistry] Reloaded %d modules", #modules)

    return true
end

function ModuleRegistry.reloadPackage(prefix)
    local modules = {}

    for name in pairs(ModuleRegistry.modules) do
        if name == prefix
            or name:sub(1, #prefix + 1) == prefix .. "." then
            modules[#modules + 1] = name
        end
    end

    table.sort(modules)

    for _, name in ipairs(modules) do
        if not reloadModule(name) then
            return false
        end
    end

    log("[ModuleRegistry] Package reloaded: %s", prefix)

    return true
end

return ModuleRegistry
