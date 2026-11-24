# meta-datadog

OpenEmbedded / Yocto layer providing recipes for Datadog monitoring agents on embedded Linux.

## Dependencies

This layer depends on:

- URI: `https://git.openembedded.org/openembedded-core`
  - layers: `meta`
  - branch: `scarthgap` / `walnascar`

## Quick Start

1. Add the layer to your `bblayers.conf`:
   ```bash
   bitbake-layers add-layer /path/to/meta-datadog
   ```

2. Add the Datadog IoT Agent to your image in `local.conf`:
   ```bitbake
   IMAGE_INSTALL:append = " datadog-iot-agent"
   ```

3. Provision the API key on the target device (not baked into the image). By default, `datadog.yaml` uses Datadog's native file secrets backend (`file.yaml`, Agent 7.70+) reading from `/etc/datadog-agent/secrets.yaml`:
   ```bash
   cat <<'EOF' > /etc/datadog-agent/secrets.yaml
   datadog_api_key: "<YOUR_DATADOG_API_KEY>"
   EOF
   chown dd-agent:dd-agent /etc/datadog-agent/secrets.yaml
   chmod 0400 /etc/datadog-agent/secrets.yaml
   ```

4. Restart the agent:
   - **systemd:** `systemctl restart datadog-agent`
   - **SysVinit:** `/etc/init.d/datadog-agent restart`

## Configuration Variables

Override any of these in `local.conf` or a `datadog-iot-agent_%.bbappend`:

| Variable | Default | Description |
| --- | --- | --- |
| `DD_SITE` | `datadoghq.com` | Datadog intake site (e.g. `datadoghq.eu`, `us5.datadoghq.com`) |
| `DD_SECRETS_BACKEND` | `file.yaml` | Native secret backend type (`file.yaml`, `file.json`, `file.text`) |
| `DD_SECRETS_FILE` | `/etc/datadog-agent/secrets.yaml` | Path on target device for secrets file |
| `DD_API_KEY_SECRET_ID` | `datadog_api_key` | Secret key name referenced in `ENC[...]` |

## Updating Versions & Checksums

Run `./scripts/fetch-deb-metadata.sh` to look up the latest `datadog-iot-agent` release URLs and SHA256 checksums across `arm64`, `armhf`, and `amd64`.

## License

Layer metadata and recipes are licensed under the MIT license (see [COPYING.MIT](COPYING.MIT)). The Datadog IoT Agent binary is licensed under Apache-2.0.
