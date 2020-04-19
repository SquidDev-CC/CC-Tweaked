describe("The settings library", function()
    describe("settings.undefine", function()
        it("clears defined settings", function()
            settings.define("test.unset", { default = 123 })
            settings.undefine("test.unset")
            expect(settings.get("test.unset")):eq(nil)
        end)
    end)

    describe("settings.set", function()
        it("validates arguments", function()
            settings.set("test", 1)
            settings.set("test", "")
            settings.set("test", {})
            settings.set("test", false)

            expect.error(settings.set, nil):eq("bad argument #1 (expected string, got nil)")
            expect.error(settings.set, "", nil):eq("bad argument #2 (expected number, string, boolean or table, got nil)")
        end)

        it("prevents storing unserialisable types", function()
            expect.error(settings.set, "", { print }):eq("Cannot serialize type function")
        end)

        it("setting changes the value", function()
            local random = math.random(1, 0x7FFFFFFF)
            settings.set("test", random)
            expect(settings.get("test")):eq(random)
        end)

        it("setting fires an event", function()
            settings.clear()

            local s = stub(os, "queueEvent")
            settings.set("test", 1)
            settings.set("test", 2)

            expect(s):called_with("setting_changed", "test", 1, nil)
            expect(s):called_with("setting_changed", "test", 2, 1)
        end)
    end)

    describe("settings.get", function()
        it("validates arguments", function()
            settings.get("test")
            expect.error(settings.get, nil):eq("bad argument #1 (expected string, got nil)")
        end)

        it("returns the default", function()
            expect(settings.get("test.undefined")):eq(nil)
            expect(settings.get("test.undefined", "?")):eq("?")

            settings.define("test.unset", { default = "default" })
            expect(settings.get("test.unset")):eq("default")
            expect(settings.get("test.unset", "?")):eq("?")
        end)
    end)

    describe("settings.getDetails", function()
        it("validates arguments", function()
            expect.error(settings.getDetails, nil):eq("bad argument #1 (expected string, got nil)")
        end)

        it("works on undefined and unset values", function()
            expect(settings.getDetails("test.undefined")):same { value = nil, changed = false }
        end)

        it("works on undefined but set values", function()
            settings.set("test", 456)
            expect(settings.getDetails("test")):same { value = 456, changed = true }
        end)

        it("works on defined but unset values", function()
            settings.define("test.unset", { default = 123, description = "A description" })
            expect(settings.getDetails("test.unset")):same
                { default = 123, value = 123, changed = false, description = "A description" }
        end)

        it("works on defined and set values", function()
            settings.define("test.defined", { default = 123, description = "A description" })
            settings.set("test.defined", 456)
            expect(settings.getDetails("test.defined")):same
                { default = 123, value = 456, changed = true, description = "A description" }
        end)
    end)

    describe("settings.unset", function()
        it("validates arguments", function()
            settings.unset("test")
            expect.error(settings.unset, nil):eq("bad argument #1 (expected string, got nil)")
        end)

        it("unsetting resets the value", function()
            settings.set("test", true)
            settings.unset("test")
            expect(settings.get("test")):eq(nil)
        end)

        it("unsetting does not touch defaults", function()
            settings.define("test.defined", { default = 123 })
            settings.set("test.defined", 456)
            settings.unset("test.defined")
            expect(settings.get("test.defined")):eq(123)
        end)

        it("unsetting fires an event", function()
            settings.set("test", 1)

            local s = stub(os, "queueEvent")
            settings.unset("test")
            expect(s):called_with("setting_changed", "test", nil, 1)
        end)
    end)

    describe("settings.clear", function()
        it("clearing resets all values", function()
            settings.set("test", true)
            settings.clear()
            expect(settings.get("test")):eq(nil)
        end)

        it("clearing does not touch defaults", function()
            settings.define("test.defined", { default = 123 })
            settings.set("test.defined", 456)
            settings.clear()
            expect(settings.get("test.defined")):eq(123)
        end)

        it("clearing fires an event", function()
            settings.set("test", 1)

            local s = stub(os, "queueEvent")
            settings.clear()
            expect(s):called_with("setting_changed", "test", nil, 1)
        end)
    end)

    describe("settings.load", function()
        it("validates arguments", function()
            expect.error(settings.load, 1):eq("bad argument #1 (expected string, got number)")
        end)

        it("defaults to .settings", function()
            local s = stub(fs, "open")
            settings.load()
            expect(s):called_with(".settings", "r")
        end)
    end)

    describe("settings.save", function()
        it("validates arguments", function()
            expect.error(settings.save, 1):eq("bad argument #1 (expected string, got number)")
        end)

        it("defaults to .settings", function()
            local s = stub(fs, "open")
            settings.save()
            expect(s):called_with(".settings", "w")
        end)
    end)
end)
