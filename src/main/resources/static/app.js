(function () {
    "use strict";

    const page = document.body.dataset.page;
    const toast = document.getElementById("toast");
    let toastTimer;

    class ApiError extends Error {
        constructor(status, message) {
            super(message);
            this.status = status;
        }
    }

    function pathPart(value) {
        return encodeURIComponent(value);
    }

    function queryPart(value) {
        return encodeURIComponent(value);
    }

    async function api(path, options = {}) {
        const init = {
            method: options.method || "GET",
            headers: {},
        };

        if (options.body !== undefined) {
            init.headers["Content-Type"] = "application/json";
            init.body = JSON.stringify(options.body);
        }

        const response = await fetch(path, init);
        if (!response.ok) {
            throw new ApiError(response.status, describeStatus(response.status));
        }

        if (response.status === 204) {
            return null;
        }

        const contentType = response.headers.get("content-type") || "";
        if (contentType.includes("application/json")) {
            return response.json();
        }

        return response.text();
    }

    function describeStatus(status) {
        switch (status) {
            case 400:
                return "The request was not valid.";
            case 404:
                return "The service or item was not found.";
            case 409:
                return "This conflicts with existing service state.";
            case 500:
                return "Deployko reported an internal failure.";
            default:
                return `Request failed with HTTP ${status}.`;
        }
    }

    function showToast(message) {
        clearTimeout(toastTimer);
        toast.textContent = message;
        toast.hidden = false;
        toastTimer = setTimeout(() => {
            toast.hidden = true;
        }, 3600);
    }

    function showStatus(element, message, kind = "info") {
        element.textContent = message;
        element.className = `status ${kind}`;
        element.hidden = false;
    }

    function clearStatus(element) {
        element.hidden = true;
        element.textContent = "";
        element.className = "status";
    }

    function setBusy(button, busy) {
        if (!button) {
            return;
        }
        button.disabled = busy;
    }

    function readForm(form) {
        return Object.fromEntries(new FormData(form).entries());
    }

    function resetForm(form) {
        form.reset();
        form.querySelectorAll("input[type='checkbox']").forEach(input => {
            input.checked = false;
        });
    }

    function serviceUrl(serviceName, suffix) {
        return `/services/${pathPart(serviceName)}${suffix}`;
    }

    function sortVersions(versions) {
        const collator = new Intl.Collator(undefined, {
            numeric: true,
            sensitivity: "base",
        });
        return [...versions].sort((left, right) => collator.compare(right, left));
    }

    function displayStatus(status) {
        return (status || "UNKNOWN").replaceAll("_", " ");
    }

    if (page === "services") {
        initServicesPage();
    }

    if (page === "configuration") {
        initConfigurationPage();
    }

    function initServicesPage() {
        const list = document.getElementById("services-list");
        const status = document.getElementById("services-status");
        const count = document.getElementById("service-count");
        const form = document.getElementById("create-service-form");

        document.querySelector("[data-action='refresh-services']").addEventListener("click", loadServices);

        form.addEventListener("submit", async event => {
            event.preventDefault();
            const button = form.querySelector("button");
            const data = readForm(form);
            setBusy(button, true);

            try {
                await api("/services", {
                    method: "POST",
                    body: {
                        name: data.name.trim(),
                        imageRepository: data.imageRepository.trim(),
                    },
                });
                resetForm(form);
                showToast("Service created.");
                await loadServices();
            } catch (error) {
                showToast(error.message);
            } finally {
                setBusy(button, false);
            }
        });

        loadServices();

        async function loadServices() {
            clearStatus(status);
            showStatus(status, "Loading services...", "info");

            try {
                const data = await api("/services");
                const services = data.services || [];
                count.textContent = `${services.length} ${services.length === 1 ? "service" : "services"}`;
                renderServices(services);
                if (services.length === 0) {
                    showStatus(status, "No services have been created yet.", "warning");
                } else {
                    clearStatus(status);
                }
            } catch (error) {
                list.innerHTML = "";
                count.textContent = "0 services";
                showStatus(status, error.message, "error");
            }
        }

        function renderServices(services) {
            list.innerHTML = "";
            services.forEach(service => {
                const serviceName = service.name;
                const row = document.createElement("tr");
                row.innerHTML = `
                    <td><a class="service-name-link" href="/service.html?serviceName=${queryPart(serviceName)}"></a></td>
                    <td></td>
                    <td></td>
                    <td><span class="status-pill"></span></td>
                `;
                row.querySelector(".service-name-link").textContent = serviceName;
                row.children[1].textContent = service.imageRepository;
                row.children[2].textContent = service.deployedVersion || "Not deployed";
                row.querySelector(".status-pill").textContent = displayStatus(service.status);
                list.appendChild(row);
            });
        }
    }

    function initConfigurationPage() {
        const params = new URLSearchParams(window.location.search);
        const serviceName = params.get("serviceName");
        const title = document.getElementById("service-title");
        const serviceStatus = document.getElementById("service-status");
        const imageRepository = document.getElementById("image-repository");
        const deployedVersion = document.getElementById("deployed-version");
        const runtimeStatus = document.getElementById("runtime-status");
        const environmentStatus = document.getElementById("environment-status");
        const portsStatus = document.getElementById("ports-status");
        const volumesStatus = document.getElementById("volumes-status");
        const networksStatus = document.getElementById("networks-status");

        async function loadVersions(button, serviceName) {
            setBusy(button, true);
            try {
                const data = await api(serviceUrl(serviceName, "/versions"));
                const versions = data.imageVersions || [];
                const select = document.querySelector("[data-role='version-select']");
                select.innerHTML = "";
                if (versions.length === 0) {
                    select.innerHTML = "<option value=''>No versions found</option>";
                    showToast("No image versions were found.");
                    return;
                }

                sortVersions(versions).forEach(version => {
                    const option = document.createElement("option");
                    option.value = version;
                    option.textContent = version;
                    select.appendChild(option);
                });
                document.querySelector("[data-role='image-version']").value = select.value;
                showToast("Versions synced.");
            } catch (error) {
                showToast(error.message);
            } finally {
                setBusy(button, false);
            }
        }

        async function deployService(button, serviceName) {
            const input = document.querySelector("[data-role='image-version']");
            const imageVersion = input.value.trim();
            if (!imageVersion) {
                showToast("Enter an image version before deploying.");
                input.focus();
                return;
            }

            await postRuntime(button, serviceName, "/runtime/deploy", "Deployment requested.", { imageVersion });
        }

        async function postRuntime(button, serviceName, suffix, successMessage, body) {
            setBusy(button, true);
            try {
                await api(serviceUrl(serviceName, suffix), {
                    method: "POST",
                    body,
                });
                showToast(successMessage);
            } catch (error) {
                showToast(error.message);
            } finally {
                setBusy(button, false);
            }
        }

        async function deleteService(button, serviceName) {
            if (!window.confirm(`Delete service "${serviceName}"?`)) {
                return;
            }

            setBusy(button, true);
            try {
                await api(serviceUrl(serviceName, ""), { method: "DELETE" });
                showToast("Service deleted.");
                window.location.href = "/index.html";
            } catch (error) {
                showToast(error.message);
            } finally {
                setBusy(button, false);
            }
        }

        async function uninstallService(button, serviceName) {
            if (!window.confirm(`Uninstall service "${serviceName}"?`)) {
                return;
            }

            await postRuntime(button, serviceName, "/runtime/uninstall", "Service uninstalled.");
        }

        if (!serviceName) {
            title.textContent = "Missing service";
            showStatus(environmentStatus, "No serviceName query parameter was provided.", "error");
            return;
        }

        title.textContent = serviceName;
        document.querySelector("[data-action='refresh-config']").addEventListener("click", loadConfiguration);
        document.querySelector("[data-action='load-versions']").addEventListener("click", event => {
            loadVersions(event.currentTarget, serviceName);
        });
        document.querySelector("[data-action='deploy']").addEventListener("click", async event => {
            await deployService(event.currentTarget, serviceName);
            await loadServiceSummary();
        });
        document.querySelector("[data-action='start']").addEventListener("click", async event => {
            await postRuntime(event.currentTarget, serviceName, "/runtime/start", "Service start requested.");
            await loadServiceSummary();
        });
        document.querySelector("[data-action='stop']").addEventListener("click", async event => {
            await postRuntime(event.currentTarget, serviceName, "/runtime/stop", "Service stop requested.");
            await loadServiceSummary();
        });
        document.querySelector("[data-action='uninstall']").addEventListener("click", async event => {
            await uninstallService(event.currentTarget, serviceName);
            await loadServiceSummary();
        });
        document.querySelector("[data-action='delete']").addEventListener("click", event => {
            deleteService(event.currentTarget, serviceName);
        });
        document.querySelector("[data-role='version-select']").addEventListener("change", event => {
            if (event.currentTarget.value) {
                document.querySelector("[data-role='image-version']").value = event.currentTarget.value;
            }
        });
        document.getElementById("environment-form").addEventListener("submit", createEnvironmentVariable);
        document.getElementById("port-form").addEventListener("submit", createPortMapping);
        document.getElementById("volume-form").addEventListener("submit", createVolumeMount);
        document.getElementById("network-form").addEventListener("submit", createNetworkAttachment);

        document.getElementById("environment-table").addEventListener("click", handleEnvironmentAction);
        document.getElementById("ports-table").addEventListener("click", handlePortAction);
        document.getElementById("volumes-table").addEventListener("click", handleVolumeAction);
        document.getElementById("networks-table").addEventListener("click", handleNetworkAction);

        loadConfiguration();

        async function loadConfiguration() {
            await loadServiceSummary();
            await Promise.all([
                loadEnvironmentVariables(),
                loadPortMappings(),
                loadVolumeMounts(),
                loadNetworkAttachments(),
            ]);
        }

        async function loadServiceSummary() {
            clearStatus(serviceStatus);
            try {
                const data = await api("/services");
                const service = (data.services || []).find(candidate => candidate.name === serviceName);
                if (!service) {
                    imageRepository.textContent = "Unknown";
                    deployedVersion.textContent = "Not deployed";
                    runtimeStatus.textContent = "Missing service";
                    showStatus(serviceStatus, "The service was not found.", "error");
                    return;
                }

                imageRepository.textContent = service.imageRepository;
                deployedVersion.textContent = service.deployedVersion || "Not deployed";
                runtimeStatus.textContent = displayStatus(service.status);
            } catch (error) {
                showStatus(serviceStatus, error.message, "error");
            }
        }

        async function loadEnvironmentVariables() {
            clearStatus(environmentStatus);
            try {
                const rows = await api(serviceUrl(serviceName, "/runtime-configuration/environment-variables"));
                renderEnvironmentVariables(rows || []);
            } catch (error) {
                renderEnvironmentVariables([]);
                showStatus(environmentStatus, error.message, "error");
            }
        }

        async function loadPortMappings() {
            clearStatus(portsStatus);
            try {
                const rows = await api(serviceUrl(serviceName, "/runtime-configuration/port-mappings"));
                renderPortMappings(rows || []);
            } catch (error) {
                renderPortMappings([]);
                showStatus(portsStatus, error.message, "error");
            }
        }

        async function loadVolumeMounts() {
            clearStatus(volumesStatus);
            try {
                const rows = await api(serviceUrl(serviceName, "/runtime-configuration/volume-mounts"));
                renderVolumeMounts(rows || []);
            } catch (error) {
                renderVolumeMounts([]);
                showStatus(volumesStatus, error.message, "error");
            }
        }

        async function loadNetworkAttachments() {
            clearStatus(networksStatus);
            try {
                const rows = await api(serviceUrl(serviceName, "/runtime-configuration/network-attachments"));
                renderNetworkAttachments(rows || []);
            } catch (error) {
                renderNetworkAttachments([]);
                showStatus(networksStatus, error.message, "error");
            }
        }

        function renderEnvironmentVariables(rows) {
            const table = document.getElementById("environment-table");
            table.innerHTML = "";
            if (rows.length === 0) {
                table.innerHTML = `<tr><td colspan="3" class="empty-state">No environment variables configured.</td></tr>`;
                return;
            }

            rows.forEach(row => {
                const tr = document.createElement("tr");
                tr.dataset.key = row.key;
                tr.innerHTML = `
                    <td></td>
                    <td><input data-role="env-value" value=""></td>
                    <td>
                        <div class="row-actions">
                            <button class="secondary" type="button" data-action="update-env">Update</button>
                            <button class="danger" type="button" data-action="delete-env">Delete</button>
                        </div>
                    </td>
                `;
                tr.children[0].textContent = row.key;
                tr.querySelector("[data-role='env-value']").value = row.value;
                table.appendChild(tr);
            });
        }

        function renderPortMappings(rows) {
            const table = document.getElementById("ports-table");
            table.innerHTML = "";
            if (rows.length === 0) {
                table.innerHTML = `<tr><td colspan="3" class="empty-state">No port mappings configured.</td></tr>`;
                return;
            }

            rows.forEach(row => {
                const tr = document.createElement("tr");
                tr.dataset.hostProtocol = row.hostProtocol;
                tr.dataset.hostPort = row.hostPort;
                tr.innerHTML = `
                    <td></td>
                    <td></td>
                    <td>
                        <div class="row-actions">
                            <button class="danger" type="button" data-action="delete-port">Delete</button>
                        </div>
                    </td>
                `;
                tr.children[0].textContent = `${row.hostProtocol} ${row.hostPort}`;
                tr.children[1].textContent = `${row.containerProtocol} ${row.containerPort}`;
                table.appendChild(tr);
            });
        }

        function renderVolumeMounts(rows) {
            const table = document.getElementById("volumes-table");
            table.innerHTML = "";
            if (rows.length === 0) {
                table.innerHTML = `<tr><td colspan="5" class="empty-state">No volume mounts configured.</td></tr>`;
                return;
            }

            rows.forEach(row => {
                const tr = document.createElement("tr");
                tr.dataset.targetPath = row.targetPath;
                tr.innerHTML = `
                    <td></td>
                    <td>
                        <select data-role="volume-type">
                            <option value="BIND">BIND</option>
                            <option value="VOLUME">VOLUME</option>
                        </select>
                    </td>
                    <td><input data-role="volume-source" value=""></td>
                    <td>
                        <label class="checkbox-label">
                            <input data-role="volume-readonly" type="checkbox">
                            Read only
                        </label>
                    </td>
                    <td>
                        <div class="row-actions">
                            <button class="secondary" type="button" data-action="update-volume">Update</button>
                            <button class="danger" type="button" data-action="delete-volume">Delete</button>
                        </div>
                    </td>
                `;
                tr.children[0].textContent = row.targetPath;
                tr.querySelector("[data-role='volume-type']").value = row.mountType;
                tr.querySelector("[data-role='volume-source']").value = row.source;
                tr.querySelector("[data-role='volume-readonly']").checked = row.readOnly;
                table.appendChild(tr);
            });
        }

        function renderNetworkAttachments(rows) {
            const table = document.getElementById("networks-table");
            table.innerHTML = "";
            if (rows.length === 0) {
                table.innerHTML = `<tr><td colspan="2" class="empty-state">No network attachments configured.</td></tr>`;
                return;
            }

            rows.forEach(row => {
                const tr = document.createElement("tr");
                tr.dataset.networkName = row.networkName;
                tr.innerHTML = `
                    <td></td>
                    <td>
                        <div class="row-actions">
                            <button class="danger" type="button" data-action="delete-network">Delete</button>
                        </div>
                    </td>
                `;
                tr.children[0].textContent = row.networkName;
                table.appendChild(tr);
            });
        }

        async function createEnvironmentVariable(event) {
            event.preventDefault();
            const form = event.target;
            const button = form.querySelector("button");
            const data = readForm(form);
            setBusy(button, true);
            try {
                await api(serviceUrl(serviceName, "/runtime-configuration/environment-variables"), {
                    method: "POST",
                    body: {
                        key: data.key.trim(),
                        value: data.value,
                    },
                });
                resetForm(form);
                showToast("Environment variable added.");
                await loadEnvironmentVariables();
            } catch (error) {
                showToast(error.message);
            } finally {
                setBusy(button, false);
            }
        }

        async function createPortMapping(event) {
            event.preventDefault();
            const form = event.target;
            const button = form.querySelector("button");
            const data = readForm(form);
            setBusy(button, true);
            try {
                await api(serviceUrl(serviceName, "/runtime-configuration/port-mappings"), {
                    method: "POST",
                    body: {
                        hostPort: Number(data.hostPort),
                        hostProtocol: data.hostProtocol,
                        containerPort: Number(data.containerPort),
                        containerProtocol: data.containerProtocol,
                    },
                });
                resetForm(form);
                showToast("Port mapping added.");
                await loadPortMappings();
            } catch (error) {
                showToast(error.message);
            } finally {
                setBusy(button, false);
            }
        }

        async function createVolumeMount(event) {
            event.preventDefault();
            const form = event.target;
            const button = form.querySelector("button");
            const data = readForm(form);
            setBusy(button, true);
            try {
                await api(serviceUrl(serviceName, "/runtime-configuration/volume-mounts"), {
                    method: "POST",
                    body: {
                        targetPath: data.targetPath.trim(),
                        mountType: data.mountType,
                        source: data.source.trim(),
                        readOnly: Boolean(data.readOnly),
                    },
                });
                resetForm(form);
                showToast("Volume mount added.");
                await loadVolumeMounts();
            } catch (error) {
                showToast(error.message);
            } finally {
                setBusy(button, false);
            }
        }

        async function createNetworkAttachment(event) {
            event.preventDefault();
            const form = event.target;
            const button = form.querySelector("button");
            const data = readForm(form);
            setBusy(button, true);
            try {
                await api(serviceUrl(serviceName, "/runtime-configuration/network-attachments"), {
                    method: "POST",
                    body: {
                        networkName: data.networkName.trim(),
                    },
                });
                resetForm(form);
                showToast("Network attachment added.");
                await loadNetworkAttachments();
            } catch (error) {
                showToast(error.message);
            } finally {
                setBusy(button, false);
            }
        }

        async function handleEnvironmentAction(event) {
            const button = event.target.closest("[data-action]");
            if (!button) {
                return;
            }

            const tr = button.closest("tr");
            const key = tr.dataset.key;

            if (button.dataset.action === "update-env") {
                setBusy(button, true);
                try {
                    await api(serviceUrl(serviceName, `/runtime-configuration/environment-variables/${pathPart(key)}`), {
                        method: "PUT",
                        body: {
                            value: tr.querySelector("[data-role='env-value']").value,
                        },
                    });
                    showToast("Environment variable updated.");
                    await loadEnvironmentVariables();
                } catch (error) {
                    showToast(error.message);
                } finally {
                    setBusy(button, false);
                }
            }

            if (button.dataset.action === "delete-env") {
                await deleteAndReload(
                    button,
                    serviceUrl(serviceName, `/runtime-configuration/environment-variables/${pathPart(key)}`),
                    "Environment variable deleted.",
                    loadEnvironmentVariables
                );
            }
        }

        async function handlePortAction(event) {
            const button = event.target.closest("[data-action='delete-port']");
            if (!button) {
                return;
            }

            const tr = button.closest("tr");
            await deleteAndReload(
                button,
                serviceUrl(
                    serviceName,
                    `/runtime-configuration/port-mappings/${pathPart(tr.dataset.hostProtocol)}/${pathPart(tr.dataset.hostPort)}`
                ),
                "Port mapping deleted.",
                loadPortMappings
            );
        }

        async function handleVolumeAction(event) {
            const button = event.target.closest("[data-action]");
            if (!button) {
                return;
            }

            const tr = button.closest("tr");
            const targetPath = tr.dataset.targetPath;

            if (button.dataset.action === "update-volume") {
                setBusy(button, true);
                try {
                    await api(
                        serviceUrl(
                            serviceName,
                            `/runtime-configuration/volume-mounts?targetPath=${queryPart(targetPath)}`
                        ),
                        {
                            method: "PUT",
                            body: {
                                mountType: tr.querySelector("[data-role='volume-type']").value,
                                source: tr.querySelector("[data-role='volume-source']").value.trim(),
                                readOnly: tr.querySelector("[data-role='volume-readonly']").checked,
                            },
                        }
                    );
                    showToast("Volume mount updated.");
                    await loadVolumeMounts();
                } catch (error) {
                    showToast(error.message);
                } finally {
                    setBusy(button, false);
                }
            }

            if (button.dataset.action === "delete-volume") {
                await deleteAndReload(
                    button,
                    serviceUrl(serviceName, `/runtime-configuration/volume-mounts?targetPath=${queryPart(targetPath)}`),
                    "Volume mount deleted.",
                    loadVolumeMounts
                );
            }
        }

        async function handleNetworkAction(event) {
            const button = event.target.closest("[data-action='delete-network']");
            if (!button) {
                return;
            }

            const tr = button.closest("tr");
            await deleteAndReload(
                button,
                serviceUrl(
                    serviceName,
                    `/runtime-configuration/network-attachments/${pathPart(tr.dataset.networkName)}`
                ),
                "Network attachment deleted.",
                loadNetworkAttachments
            );
        }

        async function deleteAndReload(button, url, message, reload) {
            setBusy(button, true);
            try {
                await api(url, { method: "DELETE" });
                showToast(message);
                await reload();
            } catch (error) {
                showToast(error.message);
            } finally {
                setBusy(button, false);
            }
        }
    }
})();
