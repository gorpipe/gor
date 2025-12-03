package org.gorpipe.security.cred;

import com.google.common.base.CaseFormat;
import org.gorpipe.base.security.BundledCredentials;
import org.gorpipe.base.security.CredentialsParser;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by villi on 13/08/16.
 */
public class CredentialsHelperMain {
    static class Options {
        public String forProject;
        public String forUserName;
        public String forUserId;
        public String forService;
        public String lookupKey;
        public boolean base64;
        public boolean sec;
        public String apiUrl;
        public String apiUser;
        public String apiPassword;

        public static Options parse(String[] args, List<String> unparsed) {
            Options options = new Options();
            int i = 0;
            while (i < args.length) {
                String arg = args[i];
                if (arg.equals("-?") || arg.equals("--help")) help("");
                if (arg.startsWith("--")) {
                    String memberName = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, arg.substring(2));
                    try {
                        Field field = Options.class.getField(memberName);
                        if (field.getType() == boolean.class) {
                            field.setBoolean(options, true);
                        } else {
                            i++;
                            if (i >= args.length) {
                                help("Missing parameter for option " + arg);
                            }
                            if (field.getType() == String.class) {
                                field.set(options, args[i]);
                            } else if (field.getType() == Integer.class) {
                                try {
                                    field.set(options, Integer.valueOf(args[i]));
                                } catch (NumberFormatException e) {
                                    help("Need integer value for option " + arg);
                                }
                            }
                        }
                    } catch (NoSuchFieldException e) {
                        help("Unknown option " + arg);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Should not happen", e);
                    }
                } else {
                    unparsed.add(arg);
                }
                i++;
            }
            return options;
        }

        public static Options processArgs(String[] args) {
            List<String> unparsed = new ArrayList<>();
            Options options = parse(args, unparsed);

            if (unparsed.size() > 0) help("Unknown parameter(s): " + unparsed.toString());
            if (options.forProject == null) help("Missing project name ");
            if (options.sec && options.base64) help("Specify only one of --sec or --base64");

            if (options.apiUrl == null) {
                String url = env.getenv("CSA_API_ENDPOINT");
                if (url != null) {
                    options.apiUrl = url.replaceFirst("/csa/api.*$", "/csa/");
                }
            }
            if (options.apiUser == null) options.apiUser = env.getenv("CSA_API_USER");
            if (options.apiPassword == null) options.apiPassword = env.getenv("CSA_API_PASSWORD");

            if (options.apiUrl == null) help("Unable to resolve api url");
            if (options.apiUser == null) help("Unable to resolve api user");
            if (options.apiPassword == null) help("Unable to resolve api password");
            return options;
        }

        public static void help(String message) {
            if (message != null) System.err.println(message);
            System.err.println(
                    "\nUsage: cred_helper --for-project projectName [--for-userName userName] [--for-userId userId] [--for-service service] [--lookup-key lookupKey] [--base64 | --sec] [--api-url apiUrl] [--api-user apiUser] [--api-password apiPassword] \n\n" +
                            "Fetches credentials from credentials service and prints (default as json object)\n" +
                            "projectName: internal project name of project to query\n" +
                            "userName: internal user name\n" +
                            "userId: id (numeric) of user to get credentials for\n" +
                            "service: restrict to service (e.g. dx or s3)\n" +
                            "lookupKey: provide a lookup key (e.g. bucket or dna nexus project id)\n" +
                            "--base64: provide output as base64 string\n" +
                            "--sec: provide output string usable as gor security context (e.g. after 'gor -Z' in gorpipe)\n\n" +
                            "apiUrl: url of credential service with trailing slash - e.g. https://dev.nextcode.com/csa/\n" +
                            "             If not specified - will derive it from environment variable CSA_API_ENDPOINT\n" +
                            "apiUser: Api user (falls back to env variable CSA_API_USER\n" +
                            "apiPassword: Api passsword (falls back to env variable CSA_API_PASSWORD"
            );
            System.exit(-1);
        }

    }

    /**
     * For testing purposes
     */
    static class Env {
        public String getenv(String env) {
            return System.getenv(env);
        }
    }

    static Env env = new Env();

    static void setEnv(Env newEnv) {
        env = newEnv;
    }


    public static void main(String[] args) throws IOException {
        Options options = Options.processArgs(args);
        CsaAuthConfiguration config = new CsaAuthConfiguration() {
            @Override
            public String getAuthApiEndpoint() {
                return options.apiUrl;
            }

            @Override
            public String getUser() {
                return options.apiUser;
            }

            @Override
            public String getPassword() {
                return options.apiPassword;
            }
        };
        CsaCredentialService service = new CsaCredentialService(config, null, new CredentialsParser(), null);
        BundledCredentials bundle = service.getCredentialsBundle(options.forProject, options.forUserName, options.forUserId, options.forService, options.lookupKey);
        if (options.sec) {
            System.out.println("cred_bundle=" + bundle.toBase64String());
        } else if (options.base64) {
            System.out.println(bundle.toBase64String());
        } else {
            System.out.println(bundle.toJson());
        }
    }

}
