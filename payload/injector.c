struct nexus_cred {
        int usage;
            int uid; int gid;
                int suid; int sgid;
                    int euid; int egid;
                        int fsuid; int fsgid;
                        };

                        void grant_nexus_root(struct nexus_cred *creds) {
                            if (creds) {
                                    creds->uid = 0;   creds->gid = 0;
                                            creds->suid = 0;  creds->sgid = 0;
                                                    creds->euid = 0;  creds->egid = 0;
                                                            creds->fsuid = 0; creds->fsgid = 0;
                                                                }
                                                                }
                                                                
}