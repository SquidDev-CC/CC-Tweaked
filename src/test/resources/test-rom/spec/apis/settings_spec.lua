describe("The settings library", function()
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
    end)

    describe("settings.get", function()
        it("validates arguments", function()
            settings.get("test")
            expect.error(settings.get, nil):eq("bad argument #1 (expected string, got nil)")
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
    end)

    describe("settings.clear", function()
        it("clearing resets all values", function()
            settings.set("test", true)
            settings.clear()
            expect(settings.get("test")):eq(nil)
        end)
    end)

    describe("settings.load", function()
        it("validates arguments", function()
            expect.error(settings.load, nil):eq("bad argument #1 (expected string, got nil)")
        end)
    end)

    describe("settings.save", function()
        it("validates arguments", function()
            expect.error(settings.save, nil):eq("bad argument #1 (expected string, got nil)")
        end)
    end)
end)
